import copy
from datetime import datetime
import random
import sys
import matplotlib.pyplot as plt

from src import Astar
from src.World import WorldInfo
import numpy as np

T = 1000
t = 0
M = 10
MUTANT_RATE = 0.3
MAX_ROUTE = 0


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

    def achievement(self, target: list):
        sample = list(copy.deepcopy(target))
        # ルートを閉じる
        sample.append(sample[0])
        # 道のり計算
        if t % 100 == 0:
            ## Aster
            total = self.astar.interpolation(sample)[0]
            print(total)
        else:
            ## ユークリッド距離計算
            total = 0
            for i in range(len(sample) - 1):
                total += self.astar.distance(sample[i], sample[i + 1])
        return total

    def get_genome_data(self, genomes):
        sum = 0
        max = 0
        min = sys.float_info.max
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
                if np.random.choice([True, False], p=[1 - ach, ach]):
                    result.append(genome[1])
        return result

    def one_order_fusion(self, sample1, sample2):
        # 切断箇所設定
        limit1 = random.randint(0, len(sample1))
        # sample1から残す順路を選択
        cut_route = sample1[0:limit1]
        # 先頭にディープコピー
        result = list(copy.deepcopy(cut_route))
        # 追加するリストをコピー
        insert_route = list(copy.deepcopy(sample2))
        # cut_routeに含まれないものを後ろに追加
        while len(insert_route) > 0:
            route = insert_route.pop(0)
            # 含まれない場合
            if not route in cut_route:
                result.append(route)

        return result

    def greedy_fusion(self, sample1, sample2):

        pass

    def mutation(self, genome, probability):
        # 変異確率
        if np.random.choice([True, False], p=[probability, 1 - probability]):
            # 交換するインデックスを決定
            i1 = random.randint(0, len(genome) - 1)
            i2 = random.randint(0, len(genome) - 1)

            # 入れ替え
            tmp = genome[i1]
            genome[i1] = genome[i2]
            genome[i2] = tmp

        return genome

    def generate_next(self, current_genomes):
        next_genomes = []
        # 遺伝子選択
        selected = self.select_genomes(current_genomes)
        # エリート優遇
        next_genomes.append(selected[0])
        # M個体できるまでループ
        while len(next_genomes) < M:
            if np.random.choice([True, False], p=[0.5, 0.5]):  # 交配確率
                i1 = random.randint(0, len(selected))
                i2 = random.randint(i1, len(selected))
                result = self.one_order_fusion(selected[i1], selected[i2])
                next_genomes.append(self.mutation(result, MUTANT_RATE))
        return next_genomes

    def calc(self, targets: list):
        global t
        # 重複を削る
        targets = list(set(targets))
        # sample = copy.deepcopy(targets)
        # 経路を閉じる
        # sample.append(sample[0])
        # self.astar.interpolation(sample)
        # 遺伝子生成
        genomes = self.set_genome(targets)
        result = []
        min_g = []
        max_g = []
        ave_g = []
        # T世代ループ
        while t < T:
            t += 1
            print(t)
            sorted_genomes = self.sort_genome(genomes)
            genomes = self.generate_next(sorted_genomes)
            result = self.get_genome_data(genomes)
            max_g.append(result[0])
            ave_g.append(result[1])
            min_g.append(result[2])
        # plt.plot(max_g, label='max')
        # plt.plot(ave_g, label='average')
        # plt.plot(min_g, label='min')
        # plt.show()
        # plt.savefig('./image/test' + datetime.now().strftime("%Y%m%d-%H%M%S") + '.png')
        # plt.cla()

        result = self.sort_genome(genomes)[0][1]
        result.append(result[0])
        return result
