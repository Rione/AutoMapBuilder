import copy
import random

from src import Astar
from src.World import WorldInfo
import numpy as np

T = 100
M = 10
MUTANT_RATE = 0.009


class GeneticAlgorithm:
    def __init__(self, world_info: WorldInfo):
        self.world_info = world_info
        self.astar = Astar.Astar(self.world_info.g_nodes)

    def set_genome(self, target_ids: list):
        sample = []
        for j in range(M):
            sample.append(random.sample(target_ids, len(target_ids)))
        return sample

    '''
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
        '''

    def calc(self, targets: list):
        self.set_genome(targets)
