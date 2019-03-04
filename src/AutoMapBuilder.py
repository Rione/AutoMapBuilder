import copy
import math

import numpy as np

from src.World import Node, Edge, Road, Building


class AutoMapBuilder:
    def __init__(self, map_width: int, map_height: int):
        self.map_width = map_width
        self.map_height = map_height
        self.nodes = {}
        self.edges = {}
        self.buildings = {}
        self.roads = {}
        self.id_table = {}
        self.building_id_table = {}
        self.last_id = 0

    def edge_distance(self, id: int):
        x1 = self.nodes[self.edges[id].first_id].x
        y1 = self.nodes[self.edges[id].first_id].y
        x2 = self.nodes[self.edges[id].end_id].x
        y2 = self.nodes[self.edges[id].end_id].y
        print(math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2))

    def get_node_key(self, x: int, y: int):
        start = x + 1
        end = y + 1

        # idの重複が発生しないよう、桁を確認する。
        if self.map_height > self.map_width:
            digit = len(str(self.map_width)) - 1
        else:
            digit = len(str(self.map_height)) - 1

        if len(str(end)) == digit:
            id = 10 ** (len(str(end)) + digit) * start + end
        else:
            id = 10 ** len(str(end)) * start + end

        if not id in self.id_table:
            self.last_id += 1
            self.id_table.setdefault(id, self.last_id)

        return self.id_table[id]

    def get_edge_key(self, x1: int, y1: int, x2: int, y2: int):
        start = self.get_node_key(x1, y1)
        end = self.get_node_key(x2, y2)
        if start > end:
            id = 10 ** len(str(start)) * end + start
        else:
            id = 10 ** len(str(end)) * start + end

        if not id in self.id_table:
            self.last_id += 1
            self.id_table.setdefault(id, self.last_id)

        return self.id_table[id]

    def create_road_key(self):
        self.last_id += 1
        return self.last_id

    def create_building_key(self, array_index: int):
        if not array_index in self.building_id_table:
            self.last_id += 1
            self.building_id_table.setdefault(array_index, self.last_id)
        return self.building_id_table[array_index]

    def create_edges(self, target_x: int, target_y: int):

        def insert_edge_data(x1, y1, x2, y2):
            edge_id = self.get_edge_key(x1, y1, x2, y2)
            id1 = self.get_node_key(x1, y1)
            id2 = self.get_node_key(x2, y2)

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
        edge_ids.append(self.get_edge_key(target_x, target_y, target_x + 1, target_y))
        edge_ids.append(self.get_edge_key(target_x, target_y, target_x, target_y + 1))
        edge_ids.append(self.get_edge_key(target_x, target_y + 1, target_x + 1, target_y + 1))
        edge_ids.append(self.get_edge_key(target_x + 1, target_y + 1, target_x + 1, target_y))
        return edge_ids

    ##################################################################################

    def calc_nodes(self, map_array):
        # rescue形式に落としこむ
        # node
        for i in range(len(map_array) + 1):
            for j in range(len(map_array) + 1):
                node_id = self.get_node_key(i, j)
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
        width = map_array.shape[0]
        height = map_array.shape[1]
        # rescue形式に落としこむ
        for w in range(width):
            for h in range(height):
                map_array_id = map_array[w][h]
                # roadの場合
                if map_array_id == 0:
                    road_id = self.create_road_key()
                    # roadのedgeをリストアップ
                    self.roads.setdefault(road_id, Road.Road(road_id, self.get_edges(w, h)))
                    continue

                # buildingの場合
                building_id = self.create_building_key(map_array_id)
                # idがすでにある場合
                if building_id in self.buildings:
                    self.buildings[building_id].update_nodes(self.get_edges(w, h))
                else:
                    self.buildings.setdefault(building_id, Building.Building(building_id, self.get_edges(w, h)))

        print(self.roads)

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

    def calc_road_neighbor(self):
        # 同じエッジを持っているroadを接続
        for road_id in self.roads:
            # 全てのroadを回す
            road = self.roads[road_id]
            for edge_id in road.edge_ids:

                for sample_road_id in self.roads:
                    sample_road = self.roads[sample_road_id]
                    for sample_edge_id in sample_road.edge_ids:
                        if road_id == sample_road_id:
                            continue

                        # 同じedge_idの場合
                        if edge_id == sample_edge_id:
                            road.neighbor.setdefault(edge_id, sample_road_id)
                            sample_road.neighbor.setdefault(edge_id, road_id)

    def calc_building_neighbor(self):
        for building_id in self.buildings:
            building = self.buildings[building_id]

            check = False

            while not check:
                # buildingのエッジをひとつだけ選ぶ
                edge_id = np.random.choice(building.edge_ids)
                # 選んだエッジが含まれるroadを取得(ない場合もある
                for road_id in self.roads:
                    # 全てのroadを回す
                    road = self.roads[road_id]
                    for road_edge_id in road.edge_ids:
                        if edge_id == road_edge_id:
                            check = True
                            building.neighbor.setdefault(edge_id, road_id)
                            road.neighbor.setdefault(edge_id, building_id)
