import copy
import sys

from src import Astar
from src.World import WorldInfo


class Greedy:
    def __init__(self, world_info: WorldInfo):
        self.world_info = world_info
        self.astar = Astar.Astar(self.world_info.g_nodes)

    def calc(self, targets: list):
        # 一番近いノードに接続
        # 全てのノードを登録するまでループ
        route = []
        # 未探索ノード登録(先頭は追加しない)
        target_ids = set(copy.deepcopy(targets[1:]))
        # スタートを登録
        route.append(targets[0])
        while len(target_ids) > 0:
            print(len(route), len(target_ids))
            min = sys.float_info.max
            min_id = 0
            for id in target_ids:
                # 距離取得
                distance = self.astar.calc_distance(route[-1], id)[0]
                #distance = self.astar.distance(route[-1], id)
                # 最小距離
                if min > distance:
                    min = distance
                    min_id = id

            # 接続済みのノードを登録
            route.append(min_id)
            # 未探索リストから除外
            target_ids.discard(min_id)

        route.append(route[0])

        return route
