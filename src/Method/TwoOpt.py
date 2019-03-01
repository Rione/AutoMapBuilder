from src import Astar
from src.World import WorldInfo


class TwoOpt:
    def __init__(self, world_info: WorldInfo, cost_table: dict):
        self.world_info = world_info
        self.astar = Astar.Astar(self.world_info.g_nodes)
        self.cost_table = cost_table

    def differ_route(self, route, a_first, a_end, b_first, b_end):
        before = self.astar.get_cost(self.cost_table, route[a_first], route[a_end]) + \
                 self.astar.get_cost(self.cost_table, route[b_first], route[b_end])
        after = self.astar.get_cost(self.cost_table, route[a_first], route[b_first]) + \
                self.astar.get_cost(self.cost_table, route[a_end], route[b_end])
        if before > after:
            return True
        return False

    def change_route(self, route, a_first, a_end, b_first, b_end):
        new_path = route[a_end:b_first + 1]
        route[a_end:b_first + 1] = new_path[::-1]

    def calc(self, route: list):
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
                    if self.differ_route(route, a_first, a_end, b_first, b_end):
                        count += 1
                        self.change_route(route, a_first, a_end, b_first, b_end)


        return route
