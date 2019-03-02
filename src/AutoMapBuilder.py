import copy
import math

import numpy as np

from src.World import Node, Edge, Road, Building


class AutoMapBuilder:
    def __init__(self):
        self.road_id = 100000
        self.building_id = 900000
        self.nodes = {}
        self.edges = {}
        self.buildings = {}
        self.roads = {}

    def edge_distance(self, id: int):
        x1 = self.nodes[self.edges[id].first_id].x
        y1 = self.nodes[self.edges[id].first_id].y
        x2 = self.nodes[self.edges[id].end_id].x
        y2 = self.nodes[self.edges[id].end_id].y
        print(math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2))

    def judge_neighbor(self, map_array: np.ndarray, distance: int, target_x: int, target_y: int):
        # 値が入っているかどうか確認
        # 指定場所に値が入っているか
        if not map_array[target_x][target_y] == -1:
            return False

        # xがはみ出していないか
        if target_x - distance >= 0:
            # 上下左右斜めで確認
            if not map_array[target_x - distance][target_y] == -1:
                return False
            if target_y - distance >= 0 and not map_array[target_x - distance][target_y - distance] == -1:
                return False
            if target_y + distance < len(map_array) and not map_array[target_x - distance][target_y + distance] == -1:
                return False

        if target_y - distance >= 0 and not map_array[target_x][target_y - distance] == -1:
            return False
        if target_y + distance < len(map_array) and not map_array[target_x][target_y + distance] == -1:
            return False

        if target_x + distance < len(map_array):
            if not map_array[target_x + distance][target_y] == -1:
                return False
            if target_y - distance >= 0 and not map_array[target_x + distance][target_y - distance] == -1:
                return False
            if target_y + distance < len(map_array) and not map_array[target_x + distance][target_y + distance] == -1:
                return False

        return True

    def can_put_building(self, map_array: np.ndarray, target_x: int, target_y: int):
        # 置けるならid,置けなければすでに入っているid、２つに挟まれている場合は0を返す
        # 指定場所に値が入っているか
        if not map_array[target_x][target_y] == -1:
            return map_array[target_x][target_y]

        building_count = 0
        tmp_id = 0

        # はじめに十字に見る
        if target_x - 1 >= 0:
            if not (map_array[target_x - 1][target_y] == -1 or map_array[target_x - 1][target_y] == 0 or
                    map_array[target_x - 1][target_y] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x - 1][target_y]

        if target_x + 1 < len(map_array):
            if not (map_array[target_x + 1][target_y] == -1 or map_array[target_x + 1][target_y] == 0 or
                    map_array[target_x + 1][target_y] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x + 1][target_y]

        if target_y - 1 >= 0:
            if not (map_array[target_x][target_y - 1] == -1 or map_array[target_x][target_y - 1] == 0 or
                    map_array[target_x][target_y - 1] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x][target_y - 1]

        if target_y + 1 < len(map_array):
            if not (map_array[target_x][target_y + 1] == -1 or map_array[target_x][target_y + 1] == 0 or
                    map_array[target_x][target_y + 1] == tmp_id):
                building_count += 1
                tmp_id = map_array[target_x][target_y + 1]

        if not building_count == 1:
            return 0

        # 斜め
        diagonal_count = 0
        if target_y - 1 >= 0:
            if target_x - 1 >= 0:
                if not (map_array[target_x - 1][target_y - 1] == -1 or map_array[target_x - 1][target_y - 1] == 0 or
                        map_array[target_x - 1][target_y - 1] == tmp_id):
                    diagonal_count += 1
            if target_x + 1 < len(map_array):
                if not (map_array[target_x + 1][target_y - 1] == -1 or map_array[target_x + 1][target_y - 1] == 0 or
                        map_array[target_x + 1][target_y - 1] == tmp_id):
                    diagonal_count += 1

        if target_y + 1 < len(map_array):
            if target_x - 1 >= 0:
                if not (map_array[target_x - 1][target_y + 1] == -1 or map_array[target_x - 1][target_y + 1] == 0 or
                        map_array[target_x - 1][target_y + 1] == tmp_id):
                    diagonal_count += 1
            if target_x + 1 < len(map_array):
                if not (map_array[target_x + 1][target_y + 1] == -1 or map_array[target_x + 1][target_y + 1] == 0 or
                        map_array[target_x + 1][target_y + 1] == tmp_id):
                    diagonal_count += 1

        if building_count == 0 and diagonal_count == 4:
            return 0
        if building_count == 0:
            return -1
        if diagonal_count == 0:
            return tmp_id
        return 0

    def judge_filled(self, array_map: np.ndarray):
        # 全て埋まったか
        for i in range(len(array_map)):
            if -1 in array_map[i]:
                return False
        return True

    def create_node_key(self, x: int, y: int):
        start = x + 1
        end = y + 1

        if len(str(end)) == 1:
            id = 10 ** (len(str(end)) + 1) * start + end
        else:
            id = 10 ** len(str(end)) * start + end
        return id

    def create_edge_key(self, x1: int, y1: int, x2: int, y2: int):
        start = self.create_node_key(x1, y1)
        end = self.create_node_key(x2, y2)
        if start > end:
            return 10 ** len(str(start)) * end + start
        else:
            return 10 ** len(str(end)) * start + end

    def create_road_key(self):
        self.road_id += 1
        return self.road_id

    def create_building_key(self, array_index: int):
        return self.building_id + array_index

    def create_edges(self, target_x: int, target_y: int):

        def insert_edge_data(x1, y1, x2, y2):
            edge_id = self.create_edge_key(x1, y1, x2, y2)
            id1 = self.create_node_key(x1, y1)
            id2 = self.create_node_key(x2, y2)

            node1 = self.nodes[id1]
            node2 = self.nodes[id2]

            # IDの大小を保証
            if node1.id > node2.id:
                tmp_node = node2
                node2 = node1
                node1 = tmp_node

            return edge_id, Edge.Edge(edge_id, node1.id, node2.id)

        # 配列のindexから座標とkeyを生成、そこから更にエッジを生成する
        edge_keys = {}
        tmp = insert_edge_data(target_x, target_y, target_x + 1, target_y)
        edge_keys.setdefault(tmp[0], tmp[1])
        tmp = insert_edge_data(target_x, target_y, target_x, target_y + 1)
        edge_keys.setdefault(tmp[0], tmp[1])
        tmp = insert_edge_data(target_x, target_y + 1, target_x + 1, target_y + 1)
        edge_keys.setdefault(tmp[0], tmp[1])
        tmp = insert_edge_data(target_x + 1, target_y + 1, target_x + 1, target_y)
        edge_keys.setdefault(tmp[0], tmp[1])

        return edge_keys

    def get_edges(self, target_x: int, target_y: int):
        edge_ids = []
        edge_ids.append(self.create_edge_key(target_x, target_y, target_x + 1, target_y))
        edge_ids.append(self.create_edge_key(target_x, target_y, target_x, target_y + 1))
        edge_ids.append(self.create_edge_key(target_x, target_y + 1, target_x + 1, target_y + 1))
        edge_ids.append(self.create_edge_key(target_x + 1, target_y + 1, target_x + 1, target_y))
        return edge_ids

    ##################################################################################

    def make_map_array(self, number_of_buildings: int, map_width: int, map_height: int):
        building_count = 0
        building_id = 1
        map_array = np.full((map_width, map_height), -1)

        while building_count < number_of_buildings:
            # 配列にランダムに拠点を設定(ただし隣接は禁止)
            # x,yをランダムで選択
            target_x = np.random.randint(len(map_array))
            target_y = np.random.randint(len(map_array))
            # 隣接がないか確認
            if self.judge_neighbor(map_array, 1, target_x, target_y):
                map_array[target_x][target_y] = building_id
                building_id += 1
                building_count += 1

        # 占領開始
        while not self.judge_filled(map_array):
            for i in range(len(map_array)):
                for j in range(len(map_array)):
                    map_array[i][j] = self.can_put_building(map_array, i, j)

        return map_array

    def calc_nodes(self, map_array):
        # rescue形式に落としこむ
        # node
        for i in range(len(map_array) + 1):
            for j in range(len(map_array) + 1):
                node_id = self.create_node_key(i, j)
                # 追加
                self.nodes.setdefault(node_id, Node.Node(node_id, i, j))

    def calc_edges(self, map_array):
        # rescue形式に落としこむ
        # edges
        for i in range(len(map_array)):
            for j in range(len(map_array)):
                edges = self.create_edges(i, j)
                for edge_id in edges:
                    # 追加
                    self.edges.setdefault(edge_id, edges[edge_id])
        print(self.edges)

    def calc_world(self, map_array):
        # rescue形式に落としこむ
        for i in range(len(map_array)):
            for j in range(len(map_array)):
                map_array_id = map_array[i][j]
                # roadの場合
                if map_array_id == 0:
                    road_id = self.create_road_key()
                    # roadのedgeをリストアップ
                    self.roads.setdefault(road_id, Road.Road(road_id, self.get_edges(i, j)))

                # buildingの場合
                building_id = self.create_building_key(map_array_id)
                # idがすでにある場合
                if building_id in self.buildings:
                    self.buildings[building_id].update_nodes(self.get_edges(i, j))
                else:
                    self.buildings.setdefault(building_id, Building.Building(building_id, self.get_edges(i, j)))

        print(self.buildings)

        new_edges = {}
        for road_id in self.roads:
            for edge_id in self.roads[road_id].edge_ids:
                new_edges.setdefault(edge_id, self.edges[edge_id])
        for building_id in self.buildings:
            for edge_id in self.buildings[building_id].edge_ids:
                new_edges.setdefault(edge_id, self.edges[edge_id])
        # エッジリスト更新
        self.edges.clear()
        self.edges = copy.deepcopy(new_edges)
