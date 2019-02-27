from src import Astar
from src.World import WorldInfo
import numpy as np


class Annealing:
    def __init__(self, world_info: WorldInfo, cost_table: dict):
        self.world_info = world_info
        self.astar = Astar.Astar(self.world_info.g_nodes)
        self.cost_table = cost_table
        # 擬似温度
        self.temperature = 1

    def calc(self, route: list):
        # 冷えるまで
        good = 0
        bud = 0
        while self.temperature > 0.01:
            print(self.temperature)
            # 入れ替え対象ペアの先頭選択(ただし終端は除外)
            for a in range(len(route) - 1):
                # print(a)
                # 選択されたindex
                a_first = a
                a_end = a + 1

                # もう一方の入れ替え対象ペアの先頭を選択
                for b in range(a + 2, len(route) - 1):
                    # 選択されたindex
                    b_first = b
                    b_end = b + 1
                    # 同じペア、もしくは連続するペアの場合は除外
                    if a == b or a_end == b_first:
                        continue
                    # 先頭と終端が同じidの為、連続するケースを除外
                    if a_end == len(route) - 1 and b_first == 0:
                        continue
                    if b_end == len(route) - 1 and a_first == 0:
                        continue
                    # 距離を比べる
                    before = self.astar.get_cost(self.cost_table, route[a_first], route[a_end]) + \
                             self.astar.get_cost(self.cost_table, route[b_first], route[b_end])
                    after = self.astar.get_cost(self.cost_table, route[a_first], route[b_first]) + \
                            self.astar.get_cost(self.cost_table, route[a_end], route[b_end])
                    # 確率選択
                    # 温度が高いほど距離が大きくなるように選択される
                    if np.random.choice([True, False], p=[1 - self.temperature, self.temperature]):
                        good += 1
                        # 入れ替えた方が距離が小さくなるようにする
                        if before > after:
                            # 入れ替え
                            new_path = route[a_end:b_first + 1]
                            route[a_end:b_first + 1] = new_path[::-1]
                    else:
                        bud += 1
                        # 入れ替えた方が距離が大きくなるようにする
                        if before < after:
                            # 入れ替え
                            new_path = route[a_end:b_first + 1]
                            route[a_end:b_first + 1] = new_path[::-1]

            # 温度を下げる
            ##単純比例減少
            self.temperature -= 0.001
            print(good, bud)
            good = 0
            bud = 0

        return route
