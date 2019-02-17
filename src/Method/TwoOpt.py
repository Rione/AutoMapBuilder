from src import Astar
from src.World import WorldInfo


class TwoOpt:
    def __init__(self, world_info: WorldInfo):
        self.world_info = world_info
        self.astar = Astar.Astar(self.world_info.g_nodes)

    def calc(self, route: list):
        # 入れ替えが一度もなくなるまでループ
        flag = True
        while flag:
            flag = False
            # 入れ替え対象ペアの先頭選択
            for a in range(len(route)):
                print(a)
                # 一度入れ替えられている場合はbreak
                # if flag:
                #    break
                # 選択されたindex
                a_first = a
                a_end = a + 1
                # 終端の場合は最初のノードを選択
                if a_end == len(route):
                    a_end = 0

                # もう一方の入れ替え対象ペアの先頭を選択
                for b in range(len(route)):
                    # 一度入れ替えられている場合はbreak
                    # if flag:
                    #    break
                    b_first = b
                    b_end = b + 1
                    # 終端の場合は最初のノードを選択
                    if b_end == len(route):
                        b_end = 0

                    # 同じペア、もしくは連続するペアの場合は除外
                    if a == b or a_end == b_first or a_first == b_end:
                        continue
                    # 距離を比べる
                    before = self.astar.calc_distance(route[a_first], route[a_end]) + self.astar.calc_distance(
                        route[b_first], route[b_end])
                    after = self.astar.calc_distance(route[a_first], route[b_first]) + self.astar.calc_distance(
                        route[a_end], route[b_end])
                    if before > after:
                        flag = True
                        print('debug')
                        # 入れ替え
                        temp = route[a_end]
                        route[a_end] = route[b_first]
                        route[b_first] = temp

        return route
