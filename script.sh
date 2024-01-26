#!/bin/bash
printf "\033[0;90;105;1mthis \033[38;5;87;3;22;4mtext \033[1;23;9;38;5;189mis\033[0;91m V\033[92mE\033[93mR\033[94mY\033[0m \033[95;100mcolor\033[97mful!\033[0m\n"
printf "this color should persist \033[92m this one\n"
printf "persist specifically on this \033[1mtext\n\033[0m"

printf "section_start:`date +%s`:script-1\r\033[0K\033[34;1mКуча сампл текстов!\033[0m\n"
printf "\033[92m"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"\033[0m       entering hibernation for 3 seconds...\n"
printf "zzz...\n"
sleep 3
printf "\033[92m"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"\033[0m       i am wide awake!\n"
for i in {1..3}
do
	printf "this is from script.sh \033[32;1msample \033[31;1mtext \033[33;1mnumber \033[36;1m${i}\033[0;m\n"
done
printf "section_end:`date +%s`:script-1\r\033[0K\n"

printf "section_start:`date +%s`:script-2\r\033[0K\033[34;1mКуча сампл текстов!\033[0m\n"
printf "\033[92m"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"\033[0m       entering hibernation for 3 seconds...\n"
printf "zzz...\n"
sleep 3
printf "\033[92m"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"\033[0m       i am wide awake!\n"
for i in {1..3}
do
        printf "this is from script.sh \033[32;1msample \033[31;1mtext \033[33;1mnumber \033[36;1m${i}\033[0;m\n"
done
printf "section_end:`date +%s`:script-2\r\033[0K\n"

printf "\033[0;90;105;1mthis \033[38;5;87;3;22;4mtext \033[1;23;9;38;5;189mis\033[0;91m V\033[92mE\033[93mR\033[94mY\033[0m \033[95;100mcolor\nful!\033[0mbut not this one\n\n"
printf "\033[92m"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"\033[0m	entering hibernation for 3 seconds...\n"
printf "zzz...\n"
sleep 3
printf "\033[92m"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"\033[0m	i am wide awake!\n"
printf "section_end:`date +%s`:bogus-section-end\r\033[0Kthis is a command to terminate section that was never opened!\n"
printf "section_start:`date +%s`:bogus-section-start\r\033[0K\033[35mthis section starts but does not end before its enclosing section does!<span>this is span injection!</span>!\033[0m\n"
printf "section_start:`date +%s`:duplicate\r\033[0K\033[35mthis section has a doppelganger!\033[0m\n"
printf "section's content"
printf "section_end:`date +%s`:duplicate\r\033[0K\n"
printf "section_start:`date +%s`:duplicate\r\033[0K\033[35mthis is the doppleganger! It should be recognized and not treated as section.\033[0m\n"
printf "doppelganger's content"
printf "section_end:`date +%s`:duplicate\r\033[0K\n"
printf "section_start:`date +%s`:duplicate-close\r\033[0K\033[35mthis section closes two times!\033[0m\n"
printf "section content"
printf "section_end:`date +%s`:duplicate-close\r\033[0K\n"
printf "section_end:`date +%s`:duplicate-close\r\033[0K\n"
