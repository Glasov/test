# asd
mport math 
class Entropy: 
    def __init__(self, number_all, number_white, number_black, number_red):
        self.number_all = number_all
        self.number_white = number_white
        self.number_black = number_black
        self.number_red = number_red
        # asdfskjdnfsdkjg
    def probability(self, value):
        return value/self.number_all
    def log2(self, value):
        return math.log10(value)
    def entropy(self, array_values):
        res = 0
        for i in range(3):
            res -= (
                self.probability(array_values[i]) *
                self.log2(self.probability(array_values[i]))
            )
            return res
