import copy
import os
import sys

import matplotlib.pyplot as plt
import xml.etree.ElementTree as ET

from src.Graph import G_Node, GraphInfo, Branch
from src.World import Edge, Road, Node, Building, WorldInfo
from src import Stack, Common
from src import GraphDrawer


class MapReader:
    def __init__(self, map_name: str):
        tree = ET.ElementTree(file=os.getcwd().replace('/src', '') + '/map/' + map_name + '/map/map.gml')
        root = tree.getroot()
        self.common = Common.Common()

        # nodes
        self.nodes = []
        for nodes in root[0]:
            id = int(dict(nodes.attrib).get('{http://www.opengis.net/gml}id'))
            x = float(str(nodes[0][0][0].text).split(',')[0])
            y = float(str(nodes[0][0][0].text).split(',')[1])
            self.nodes.append(Node.Node(id, x, y))

        # edges
        self.edges = []
        for edges in root[1]:
            id = int(dict(edges.attrib).get('{http://www.opengis.net/gml}id'))
            first_id = int(str(dict(edges[0].attrib).get('{http://www.w3.org/1999/xlink}href')).replace('#', ''))
            end_id = int(str(dict(edges[1].attrib).get('{http://www.w3.org/1999/xlink}href')).replace('#', ''))
            self.edges.append(Edge.Edge(id, first_id, end_id))

        # building
        self.buildings = []
        for buildings in root[2]:
            id = int(dict(buildings.attrib).get('{http://www.opengis.net/gml}id'))
            edge_ids = []
            neighbour_ids = []
            for building in buildings[0]:
                edge_ids.append(
                    int(str(dict(building.attrib).get('{http://www.w3.org/1999/xlink}href')).replace('#', '')))
                neighbour_id = dict(building.attrib).get('{urn:roborescue:map:gml}neighbour')
                if not neighbour_id == None:
                    neighbour_ids.append(int(neighbour_id))
            self.buildings.append(Building.Building(id, edge_ids, neighbour_ids))

        # roads
        self.roads = []
        for roads in root[3]:
            id = int(dict(roads.attrib).get('{http://www.opengis.net/gml}id'))
            edge_ids = []
            neighbour_ids = []
            for road in roads[0]:
                edge_ids.append(
                    int(str(dict(road.attrib).get('{http://www.w3.org/1999/xlink}href')).replace('#', '')))
                neighbour_id = dict(road.attrib).get('{urn:roborescue:map:gml}neighbour')
                if not neighbour_id == None:
                    neighbour_ids.append(int(neighbour_id))
            self.roads.append(Road.Road(id, edge_ids, neighbour_ids))

        self.world_info = WorldInfo.WorldInfo(self.nodes, self.edges, self.buildings, self.roads)

    def build_graph(self):
        g_nodes = self.world_info.g_nodes
        drawer = GraphDrawer.GraphDrawer(g_nodes)

        # グラフノード作成
        # 最初のノード選択
        open_list = Stack.Stack()
        for id in g_nodes:
            if len(g_nodes.get(id).neighbours) > 2:
                open_list.push(id)
                break

        # 深さ優先探索
        closed_list = Stack.Stack()
        graph_info = GraphInfo.GraphInfo()
        total_count = 0
        neighbour_number_count = 0
        entrance = 0
        while not open_list.empty():
            target_id = open_list.pop()
            closed_list.push(target_id)
            if 'Road' in str(type(g_nodes.get(target_id))):
                total_count += 1
                if len(g_nodes.get(target_id).neighbours) == 2:
                    neighbour_number_count += 1
                for neighbour in g_nodes.get(target_id).neighbours:
                    if 'Building' in str(type(neighbour)):
                        entrance += 1

            neighbour_branchs = []
            for neighbour_id in g_nodes.get(target_id).neighbour_ids:
                if not neighbour_id in closed_list.stack:
                    open_list.push(neighbour_id)
                    neighbour_branchs.append(Branch.Branch(neighbour_id, self.common.id_to_distance(target_id,
                                                                                                    neighbour_id,
                                                                                                    g_nodes)))

            graph_info.branch_list.setdefault(target_id, copy.deepcopy(neighbour_branchs))

        drawer.map_register(graph_info.branch_list)
        drawer.show_plt()
