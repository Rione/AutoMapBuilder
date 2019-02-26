from src import Astar
from src.World import WorldInfo


class TwoOpt:
    def __init__(self, world_info: WorldInfo, cost_table: dict):
        self.world_info = world_info
        self.astar = Astar.Astar(self.world_info.g_nodes)

    def calc(self, route: list, method: str):
        # 入れ替えが一度もなくなるまでループ
        count = 1
        while count > 0:
            count = 0
            # 入れ替え対象ペアの先頭選択(ただし終端は除外)
            for a in range(len(route) - 1):
                print(a)
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
                    if method == 'aster':
                        before = self.astar.calc_distance(route[a_first], route[a_end])[0] + \
                                 self.astar.calc_distance(route[b_first],
                                                          route[b_end])[0]
                        after = self.astar.calc_distance(route[a_first], route[b_first])[0] + \
                                self.astar.calc_distance(route[a_end],
                                                         route[b_end])[0]
                    else:
                        before = self.astar.raw_distance(route[a_first], route[a_end]) + \
                                 self.astar.raw_distance(route[b_first],
                                                         route[b_end])
                        after = self.astar.raw_distance(route[a_first], route[b_first]) + \
                                self.astar.raw_distance(route[a_end],
                                                        route[b_end])
                    if before > after:
                        count += 1
                        # 入れ替え
                        new_path = route[a_end:b_first + 1]
                        route[a_end:b_first + 1] = new_path[::-1]

        return route
