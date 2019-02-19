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

    def sort_genome(self, genomes):
        target_genomes = []
        # 評価値と遺伝子を統合
        for genome in genomes:
            target_genomes.append([self.achievement(genome), genome])
        # sorted_genomes = sorted(genomes, key=lambda genomes: self.achievement(genomes[:]))
        sorted_genomes = sorted(target_genomes)
        return sorted_genomes

    def achievement(self, sample: list):
        # 重複を削る
        sample = list(set(sample))
        # ルートを閉じる
        sample.append(sample[0])
        # 道のり計算
        ## Aster
        # total = self.astar.interpolation(sample)[0]
        ## ユークリッド距離計算
        total = 0
        for i in range(len(sample) - 1):
            total += self.astar.distance(sample[i], sample[i + 1])
        return total

    def get_genome_data(self, genomes):
        sum = 0
        max = 0
        min = 1
        for genome in genomes:
            ach = self.achievement(genome)
            sum += ach
            if ach > max:
                max = ach
            if ach < min:
                min = ach

        return max, sum / len(genomes), min

    def select_genomes(self, sorted_genomes: list):
        # ルーレット
        G = 0
        for genome in sorted_genomes:
            G += genome[0]

        result = []
        # エリートを格納(遺伝子のみ)
        result.append(sorted_genomes[0][1])
        while len(result) < len(sorted_genomes):
            for genome in sorted_genomes:
                # 選択割合
                ach = genome[0] / G
                print(1 - ach)
                if np.random.choice([True, False], p=[1 - ach, ach]):
                    result.append(genome[1])
        return result

    def fusion(self, sample1, sample2):
        limit = random.randint(0, len(sample1))
        tmp1 = copy.copy(sample1)
        tmp2 = copy.copy(sample2)
        for i in range(limit):
            tmp1[i] = sample2[i]
            # tmp2[i] = sample1[i]
        return tmp1

    def mutation(self, genome, probability):
        for i in range(len(genome)):
            if np.random.choice([True, False], p=[probability, 1 - probability]):
                genome[i] = 2 + ~genome[i]  # not演算
        return genome

    def generate_next(self, current_genomes):
        next_genomes = []
        selected = self.select_genomes(current_genomes)
        next_genomes.append(selected[0])
        while len(next_genomes) < M:
            if np.random.choice([1, 0], p=[0.5, 0.5]):  # 交配確率
                sample = random.sample(selected, 2)
                result1 = self.fusion(sample[0], sample[1])
                next_genomes.append(self.mutation(result1, MUTANT_RATE))
        return next_genomes

    def calc(self, targets: list):
        # 遺伝子生成
        genomes = self.set_genome(targets)
        result = []
        # T世代ループ
        for t in range(T):
            sorted_genomes = self.sort_genome(genomes)
            genomes = self.generate_next(sorted_genomes)
            result = self.get_genome_data(genomes)

        return result
