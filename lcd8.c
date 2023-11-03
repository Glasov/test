// coding: cp1251


/**
@file    lcd8.c
@version 0.0
@date    2021.07.23

@author  Alexander
@email   zhevak@mail.ru


@brief   Драйвер символьного LCD, 8-разрядная шина
*/



#include <avr/io.h>
#include <stdint.h>
#include <util/delay.h>
#include <avr/pgmspace.h>

#include "hal.h"

#include "lcd8.h"



/*
E:  Chip Enable
RS: 0 - instraction
    1 - data
RW: 0 - write
    1 - read
*/

/**
  Ожидает готовности LCD
*/
void _waitForReady(void)
{
  uint8_t busyFlag;

  DDRC = 0x00;   // На ввод

  PORTD &= ~_BV(LCD_RS);  // Instraction
  PORTD |=  _BV(LCD_RW);  // Read
 
  do
  {
    PORTD |=  _BV(LCD_E);   // Строб
    PORTD |=  _BV(LCD_E);   // Строб
    busyFlag = PINC;
    PORTD &= ~_BV(LCD_E);
  }
  while ((busyFlag & 0x80) == 0x80);
  
  DDRC = 0xFF;  // На вывод
}


/**
  Записывает в LCD команду.
*/
static void _wrc(uint8_t cmd)
{
  PORTC = cmd;
  
  PORTD &= ~_BV(LCD_RS);  // Instraction
  PORTD &= ~_BV(LCD_RW);  // Write

  PORTD |=  _BV(LCD_E);   // Строб
  PORTD &= ~_BV(LCD_E);
  
  _waitForReady();
}



void lcd_init(void)
{
  //  Настройка порта произведена в hal.c

  // Инициализация дисплея
  _delay_ms(40);  // Задержка перед инициализацией LCD не менее 15 мс

  PORTC |=  0x03;        // Шина данных = 0b0011  

  PORTC |=  _BV(LCD_E);  // Строб
  PORTC &= ~_BV(LCD_E);

  _delay_ms(5);          // Задержка не менее 4.1 ms

  PORTC |=  _BV(LCD_E);  // Ещё один строб
  PORTC &= ~_BV(LCD_E);

  _delay_ms(1);          // Задержка не менее 0.1 ms

  PORTC |=  _BV(LCD_E);  // Ещё строб
  PORTC &= ~_BV(LCD_E);
  
  // Перевели шину LCD в 8-битный режим
  
  // Продолжаем настраивать работу LCD
  _wrc(0x38);  // 0b0010NF00
  _wrc(0x0C);

  _wrc(0x01);   // Команда "Clear Display"
  _delay_ms(2); // Задержка не менее 1.53 мс
  
  _wrc(0x06);  
}



// Очистить дисплей, курсор в левый верхний угол
void lcd_clear(void)
{
  _wrc(0x01);
}



//
//  Перевод кодировки Win-1251 в кодировку LDC
//
const char rus[] PROGMEM = {
// 'А',  'Б',  'В',  'Г',  'Д',  'Е',  'Ж',  'З',  'И',  'Й',  'К',  'Л',  'М',  'Н',  'О',  'П',
   0x41, 0xa0, 0x42, 0xa1, 0xe0, 0x45, 0xa3, 0xa4, 0xa5, 0xa6, 0x4b, 0xa7, 0x4d, 0x48, 0x4f, 0xa8,
// 'Р',  'С',  'Т',  'У',  'Ф',  'Х',  'Ц',  'Ч',  'Ш',  'Щ',  'Ъ',  'Ы',  'Ь',  'Э',  'Ю',  'Я',
   0x50, 0x43, 0x54, 0xa9, 0xaa, 0x58, 0xe1, 0xab, 0xac, 0xe2, 0xad, 0xae, 0x62, 0xaf, 0xb0, 0xb1,
// 'а',  'б',  'в',  'г',  'д',  'е',  'ж',  'з',  'и',  'й',  'к',  'л',  'м',  'н',  'о',  'п',
   0x61, 0xb2, 0xb3, 0xb4, 0xe3, 0x65, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xbb, 0xbc, 0xbd, 0x6f, 0xbe,
// 'р',  'с',  'т',  'у',  'ф',  'х',  'ц',  'ч',  'ш',  'щ',  'ъ',  'ы',  'ь',  'э',  'ю',  'я'
   0x70, 0x63, 0xbf, 0x79, 0xe4, 0x78, 0xe5, 0xc0, 0xc1, 0xe6, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7
};

uint8_t _translate(uint8_t data)
{
  switch (data)
  {
  case 0xA8:      // 'Ё'
    return 0xA2;
  case 0xB8:      // 'ё'
    return 0xB5;
  case 0x7B:      // '{'
    return 0xC8;
  case 0x7C:      // '|'
    return 0xD7;
  case 0x7D:      // '}'
    return 0xC9;
  case 0x7E:      // '~'
    return 0xE9;
  case 0xA7:      // '§'
    return 0xFD;
  case 0xB6:      // '¶'
    return 0xFE;
  case 0xB9:      // '№'
    return 0xCC;
  case 0x5C:      // '\\'
    return 0xFF;
  case 0xB0:      // '°'
    return 0xEF;
  }

  if (data >= 0xC0)
    return pgm_read_byte(&(rus[data - 0xC0]));
  else
    return data;
}



// Записать символ на экран в позицию курсора
void lcd_char(uint8_t data)
{
  PORTC = _translate(data);

  PORTD |=  _BV(LCD_RS);  // Data
  PORTD &= ~_BV(LCD_RW);  // Write
  
  PORTD |=  _BV(LCD_E);     // Строб
  PORTD &= ~_BV(LCD_E);

  _waitForReady();
}



// Записать байт в HEX-виде в позицию курсора
void lcd_hex(uint8_t data)
{
  uint8_t hex;

  hex = (data & 0xF0) >> 4;
  if (hex < 10)
    lcd_char(hex + '0');
  else
    lcd_char(hex - 10 + 'A');

  hex = data & 0x0f;
  if (hex < 10)
    lcd_char(hex + '0');
  else
    lcd_char(hex - 10 + 'A');
}


// Установить курсор в позицию (X, Y)
//
//  Начало строки    4*16   4*20   2*08   2*12
//  -------------    ----   ----   ----   ----
//    0-я строка     0x00   0x00   0x00   0x00
//    1-я строка     0x40   0x40   0x40   0x40
//    2-я строка     0x10   0x14
//    3-я строка     0x50   0x54
//
void lcd_gotoXY(uint8_t x, uint8_t y)
{
  uint8_t addr;

  if (y == 0)
    addr = 0x80 | (x % 20);
  else
    addr = 0x80 | (0x40 + (x % 20));
  
  _wrc(addr);
}


// Вывести символ сh в позицию (X, Y)
void lcd_charXY(uint8_t x, uint8_t y, uint8_t ch)
{
  lcd_gotoXY(x, y);
  lcd_char(ch);
}


// Вывести на экран строку str с позиции курсора
void lcd_string(uint8_t *str)
{
  while (*str != '\0')
  {
    lcd_char(*str);
    str++;
  }
}


// Вывести строку str в в строку на экране y
void lcd_stringY(uint8_t y, uint8_t *str)
{
  lcd_gotoXY(0, y);

  while (*str != '\0')
  {
    lcd_char(*str);
    str++;
  }
}


// Вывести на экран строку str с позиции (x, Y)
void lcd_stringXY(uint8_t x, uint8_t y, uint8_t *str)
{
  lcd_gotoXY(x, y);

  while (*str != '\0')
  {
    lcd_char(*str);
    str++;
  }
}


// Стереть строку номер y
void lcd_clearY(uint8_t y)
{
  uint8_t i;

  lcd_gotoXY(0, y);
  
  for (i = 0; i < XMAX; i++)
    lcd_char(' ');
}
