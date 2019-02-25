import copy
from datetime import datetime
import random
import sys
import matplotlib.pyplot as plt

from src import Astar
from src.Method import TwoOpt
from src.World import WorldInfo
import numpy as np

T = 1000
t = 0
M = 10
MUTANT_RATE = 0.01
MAX_ROUTE = 0


class GeneticAlgorithm:
    def __init__(self, world_info: WorldInfo):
        self.world_info = world_info
        self.astar = Astar.Astar(self.world_info.g_nodes)
        self.two_opt = TwoOpt.TwoOpt(self.world_info)

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
        if 0 * t % 100 == 1:
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
                ach = (G - genome[0]) / G
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

    def partially_mapped_fusion(self, sample1: list, sample2: list):
        # 切断箇所を決定
        limit1 = random.randint(0, len(sample1))
        limit2 = random.randint(limit1, len(sample1))

        # 切断箇所を抽出
        cut_list1 = sample1[limit1:limit2]
        cut_list2 = sample2[limit1:limit2]

        # 変換セット
        table = {}
        for i in range(len(cut_list1)):
            table.setdefault(cut_list1[i], cut_list2[i])

        # result
        result = []
        for i in range(len(sample1)):
            # 切断箇所内の場合
            if i >= limit1 and i < limit2:
                result.append(sample1[i])
            else:
                insert = sample2[i]
                # 変換が終わるまでループ
                while insert in table:
                    insert = table[insert]
                result.append(insert)
        return result

    def greedy_fusion(self, sample1: list, sample2: list):
        # 接続ノードを返す関数
        print(sample1)
        print(sample2)

        def connect_nodes(target_id: int):
            samples = [sample1, sample2]
            result = set()
            s1 = sample1.index(target_id)
            s2 = sample2.index(target_id)
            target_index = [s1, s2]

            for i in range(len(target_index)):
                if target_index[i] - 1 < 0:
                    result.add(samples[i][-1])
                else:
                    result.add(samples[i][target_index[i] - 1])

                if target_index[i] + 1 >= len(samples[0]):
                    result.add(samples[i][0])
                else:
                    result.add(samples[i][target_index[i] + 1])

            return result

        # 距離-選択確率テーブルを作成
        table = {}
        for i in range(len(sample1)):
            node_set = connect_nodes(sample1[i])
            result = []
            total = 0
            for n in node_set:
                distance = self.astar.distance(sample1[i], n)
                total += distance
                result.append([n, distance])
            # 選択確率に変換
            for j in range(len(result)):
                result[j][1] = (result[j][1]) / total
            table.setdefault(sample1[i], result)

        # ランダムにスタート地点を選択
        start_index = random.randint(0, len(sample1) - 1)
        result = []
        result.append(sample1[start_index])
        for i in range(len(sample1) - 1):
            # つながっているノード一覧をnpリストに突っ込む
            data_list = np.asarray(table[result[-1]])
            # sort
            data_list = np.sort(data_list, axis=1)
            # 確率とIDを分割
            ids = []
            rate = []
            for j in range(len(data_list)):
                ids.append(data_list[j][1])
                rate.append(data_list[j][0])
            # 確率依存選択
            choice = int(np.random.choice(ids, p=rate))
            delete_index = list(table[result[-1]]).index(choice)
            table[result[-1]].pop(delete_index)
            result.append(choice)

        return result

    def character_fusion(self, sample1: list, sample2: list):
        cut_list = []
        insert_list = []
        # 同じペアを探索
        for a in range(len(sample1) - 1):
            for b in range(len(sample2) - 1):
                # 比較
                ##setに入れる
                sample_set = set()
                sample_set.add(sample1[a])
                sample_set.add(sample1[a + 1])
                sample_set.add(sample2[b])
                sample_set.add(sample2[b + 1])
                ##同じペアの場合は要素が2になる
                if len(sample_set) == 2:
                    cut_list.append(sample_set.pop())
                    cut_list.append(sample_set.pop())
        # 残りの要素を探索
        for s in sample2:
            if not s in cut_list:
                insert_list.append(s)
        # 結合
        result = copy.deepcopy(sample1)
        for i in range(len(result)):
            if not result[i] in cut_list:
                result[i] = insert_list.pop(0)

        return result

    def mutation(self, genome, probability):
        # 変異確率
        if np.random.choice([True, False], p=[probability, 1 - probability]):
            # '''
            # 交換するインデックスを決定
            i1 = random.randint(0, len(genome) - 1)
            i2 = random.randint(0, len(genome) - 1)

            # 入れ替え
            tmp = genome[i1]
            genome[i1] = genome[i2]
            genome[i2] = tmp
            # '''
            # return self.two_opt.calc(genome, '')
        return genome

    def generate_next(self, current_genomes):
        next_genomes = []
        # 遺伝子選択
        selected = self.select_genomes(current_genomes)
        # エリート優遇
        next_genomes.append(selected[0])
        # M個体できるまでループ
        while len(next_genomes) < M:
            if np.random.choice([1, 0], p=[0.5, 0.5]):  # 交配確率
                sample = random.sample(selected, 2)
                result = self.greedy_fusion(sample[0], sample[1])
                # result = self.partially_mapped_fusion(sample[0], sample[1])
                # result = self.one_order_fusion(sample[0], sample[1])
                # result = self.character_fusion(sample[0], sample[1])
                next_genomes.append(self.mutation(result, MUTANT_RATE))
        return next_genomes

    def calc(self, targets: list):
        global t
        t = 0
        # 重複を削る
        targets = list(set(targets))
        # sample = copy.deepcopy(targets)
        # 経路を閉じる
        # sample.append(sample[0])
        # self.astar.interpolation(sample)
        # 遺伝子生成
        genomes = self.set_genome(targets)
        '''
        # 2-optを通す
        for i in range(len(genomes)):
            genomes[i].append(genomes[i][0])
            genomes[i] = list(set(two_opt.calc(genomes[i], '')))
            '''
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
        plt.plot(max_g, label='max')
        plt.plot(ave_g, label='average')
        plt.plot(min_g, label='min')
        plt.savefig('./image/test' + datetime.now().strftime("%Y%m%d-%H%M%S") + '.png')
        # plt.show()
        plt.cla()

        result = self.sort_genome(genomes)[0][1]
        result.append(result[0])
        return result
