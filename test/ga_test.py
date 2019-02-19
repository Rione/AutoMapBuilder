import random
import matplotlib.pyplot as plt

import numpy as np
import copy

T = 100
M = 10
MUTANT_RATE = 0.009


def set_genome(n, x):
    sample = []
    for j in range(n):
        genome = []
        for i in range(x):
            genome.append(np.random.choice([1, 0], p=[0.1, 0.9]))
        sample.append(genome)
    return sample


def sort_genome(genomes):
    sorted_genomes = sorted(genomes, key=lambda genomes: sum(genomes[:]), reverse=True)
    return sorted_genomes


def achievement(sample):
    # すべての要素が1であれば1.0を返す
    # 平均
    return sum(sample) / len(sample)


def get_genome_data(genomes):
    sum = 0
    max = 0
    min = 1
    for genome in genomes:
        ach = achievement(genome)
        sum += ach
        if ach > max:
            max = ach
        if ach < min:
            min = ach

    return max, sum / len(genomes), min


def select_elite(genomes, rate):
    # ルーレット
    list = []
    G = 0
    for genome in genomes:
        ach = achievement(genome)
        list.append(ach)
        G += ach

    for i in range(len(list)):
        list[i] = list[i] / G

    result = []
    result.append(genomes[0])
    i = 0
    while len(result) < len(genomes):
        if np.random.choice([True, False], p=[list[i], 1 - list[i]]):
            result.append(genomes[i])
        i += 1
        if len(genomes) <= i:
            i = 0
    return result


def fusion(sample1, sample2):
    limit = random.randint(0, len(sample1))
    tmp1 = copy.copy(sample1)
    tmp2 = copy.copy(sample2)
    for i in range(limit):
        tmp1[i] = sample2[i]
        # tmp2[i] = sample1[i]
    return tmp1


def mutation(genome, probability):
    for i in range(len(genome)):
        if np.random.choice([True, False], p=[probability, 1 - probability]):
            genome[i] = 2 + ~genome[i]  # not演算
    return genome


def generate_next(current_genomes):
    next_genomes = []
    elites = select_elite(current_genomes, 0.3)
    next_genomes.append(elites[0])
    while len(next_genomes) < M:
        if np.random.choice([1, 0], p=[0.5, 0.5]):  # 交配確率
            sample = random.sample(elites, 2)
            result1 = fusion(sample[0], sample[1])
            next_genomes.append(mutation(result1, MUTANT_RATE))
    return next_genomes


if __name__ == '__main__':
    # t = 0
    genomes = set_genome(M, 10)
    min_g = []
    max_g = []
    ave_g = []
    for t in range(T):
        sorted_genomes = sort_genome(genomes)
        genomes = generate_next(sorted_genomes)
        result = get_genome_data(genomes)
        max_g.append(result[0])
        ave_g.append(result[1])
        min_g.append(result[2])

    print(sort_genome(genomes))
    plt.plot(max_g, label='max')
    plt.plot(ave_g, label='average')
    plt.plot(min_g, label='min')
    plt.xlabel('generation')
    plt.ylabel('evaluation_value')
    plt.text(10, 0.2, '*Final result*\nevaluation_value = ' + str(max_g[-1]))
    plt.legend()
    plt.show()
