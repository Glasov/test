from Entropy import *

print("hello!!!")

number_all = 0
experiment1 = []
experiment2 = []
number_all = int(input())
experiment1 = [int(i) for i in input().split()]
experiment2 = [int(i) for i in input().split()]

entr_arr1 = Entropy(number_all, *experiment1)
entr_arr2 = Entropy(number_all, *experiment2)

experiment1_for_entropy1 = [entr_arr1.number_white, entr_arr1.number_black, entr_arr1.number_red]
experiment2_for_entropy2 = [entr_arr2.number_white, entr_arr2.number_black, entr_arr2.number_red]
entr1 = entr_arr1.entropy(experiment1_for_entropy1)
entr2 = entr_arr2.entropy(experiment2_for_entropy2)

if entr2 > entr1:
    print("2-й опыт имеет большую неопределенность, чем первый")
elif entr1 > entr2:
    print("1-й опыт имеет большую неопределенность, чем второй")
else:
    print("1-й и 2-й опыты имеют одинаковую неопределенность")
