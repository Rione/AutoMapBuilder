import copy
import math
import sys


class A_Node:
    def __init__(self, node_id: int, cost: float, future_cost: float, route: list):
        self.id = node_id
        self.c = cost
        self.h = future_cost
        self.total = self.c + self.h
        self.route = route


class Astar:
    def __init__(self, nodes: dict):
        self.nodes = nodes
        self.open_list = {}
        self.closed_list = {}

    def distance(self, first_id: int, end_id: int):
        first = self.nodes.get(first_id)
        end = self.nodes.get(end_id)
        X = end.x - first.x
        Y = end.y - first.y
        return math.sqrt(X ** 2 + Y ** 2)

    def calc_distance(self, first_id: int, end_id: int):
        self.open_list = {}
        self.closed_list = {}
        # 最初のノードを追加
        self.open_list.setdefault(first_id, A_Node(first_id, 0, self.distance(first_id, end_id), [first_id]))
        # openlistが空になるまでループ
        while not len(self.open_list) <= 0:
            # コスト最小のターゲット選択
            min_id = 0
            min_cost = sys.float_info.max
            for id in self.open_list:
                node = self.open_list.get(id)
                if min_cost > node.total:
                    min_cost = node.total
                    min_id = id
            target = self.open_list.pop(min_id)

            # 目的地の場合
            if target.id == end_id:
                return target.c, target.route

            # closelistに突っ込む
            self.closed_list.setdefault(target.id, copy.deepcopy(target))
            # ネイバーのノードを取得
            for neighbour in self.nodes.get(target.id).neighbours:
                # コスト計算
                ## 予測コスト
                h = self.distance(neighbour.id, end_id)

                ## 蓄積コスト
                c = target.c + self.distance(target.id, neighbour.id)

                # openlistに含まれる場合
                if neighbour.id in self.open_list:
                    # コストを比べる
                    ##openlist
                    if self.open_list.get(neighbour.id).total > h + c:
                        # 排除
                        self.open_list.pop(neighbour.id)
                        # 追加
                        ##ルート更新
                        target_route = copy.deepcopy(target.route)
                        target_route.append(neighbour.id)
                        self.open_list.setdefault(neighbour.id, A_Node(neighbour.id, c, h, target_route))

                # closelistに含まれる場合
                elif neighbour.id in self.closed_list:
                    ##closelist
                    if self.closed_list.get(neighbour.id).total > h + c:
                        # 排除
                        self.closed_list.pop(neighbour.id)
                        # 追加
                        ##ルート更新
                        target_route = copy.deepcopy(target.route)
                        target_route.append(neighbour.id)
                        self.open_list.setdefault(neighbour.id, A_Node(neighbour.id, c, h, target_route))

                # openlist and closelist に含まれていない場合
                else:
                    ##ルート更新
                    target_route = copy.deepcopy(target.route)
                    target_route.append(neighbour.id)
                    self.open_list.setdefault(neighbour.id, A_Node(neighbour.id, c, h, target_route))
        return None

    def interpolation(self, route: list):
        # 描画用に道のりルートに変換
        result = []
        total = 0
        # result.append(route[0])
        # セールスマン問題の近似解を取り出す
        for i in range(len(route) - 1):
            # 最短ルート取得（最初と終端含む）
            a_route = self.calc_distance(route[i], route[i + 1])
            total += a_route[0]
            for r in range(len(a_route[1]) - 1):
                result.append(a_route[1][r])
        result.append(route[-1])
        return total, result
