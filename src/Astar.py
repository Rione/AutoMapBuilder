import copy
import math
import sys

import numpy as np

from src import Common


class A_Node:
    def __init__(self, node_id: int, cost: float, future_cost: float):
        self.id = node_id
        self.c = cost
        self.h = future_cost
        self.total = self.c + self.h
        self.route = []


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
        # 最初のノードを追加
        self.open_list.setdefault(first_id, A_Node(first_id, 0, self.distance(first_id, end_id)))

        # openlistが空になるまでループ
        while not len(self.open_list) <= 0:
            # 目的地の場合
            if self.open_list[0].id == end_id:
                break

            # コスト最小のターゲット選択
            min_id = 0
            min_cost = sys.int_info.max
            for id in self.open_list:
                cost = self.open_list.get(id)
                if min_cost > cost:
                    min_cost = cost
                    min_id = id

            target = self.open_list.pop(min_id)
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
                        self.open_list.setdefault(neighbour.id, A_Node(neighbour.id, c, h))

                # closelistに含まれる場合
                if neighbour.id in self.closed_list:
                    ##closelist
                    if self.closed_list.get(neighbour.id).total > h + c:
                        # 排除
                        self.closed_list.pop(neighbour.id)
                        # 追加
                        self.open_list.setdefault(neighbour.id, A_Node(neighbour.id, c, h))

                # openlist and closelist に含まれていない場合
                if not neighbour.id in self.open_list and not neighbour.id in self.closed_list:
                    self.open_list.setdefault(neighbour.id, A_Node(neighbour.id, c, h))
