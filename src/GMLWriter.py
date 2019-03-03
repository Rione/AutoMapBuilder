import copy
import math
import sys

from src.World import Building, Road
import xml.etree.ElementTree as et
import xml.dom.minidom


class GMLWrite:
    def __init__(self, path: str, nodes: dict, edges: dict, buildings: dict, roads: dict):
        self.path = path
        self.nodes = nodes
        self.edges = edges
        self.buildings = buildings
        self.roads = roads

    def edge_distance(self, id: int):
        x1 = self.nodes[self.edges[id].first_id].x
        y1 = self.nodes[self.edges[id].first_id].y
        x2 = self.nodes[self.edges[id].end_id].x
        y2 = self.nodes[self.edges[id].end_id].y
        print(math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2))

    def node_distance(self, id1: int, id2: int):
        x1 = self.nodes[id1].x
        y1 = self.nodes[id1].y
        x2 = self.nodes[id2].x
        y2 = self.nodes[id2].y
        return math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2)

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
            return 10 ** len(str(start)) * end + start, "-"
        else:
            return 10 ** len(str(end)) * start + end, "+"

    def get_draw_edges(self, edge_ids: list):
        sample_edge_ids = copy.deepcopy(edge_ids)
        # ノードを辿って描画用エッジリストを作成する
        draw_edge_ids = []

        # スタート地点を設定
        draw_edge_ids.append([sample_edge_ids.pop(0), '-'])
        # 全てのエッジを辿るまでループ
        next_node_id = self.edges[draw_edge_ids[-1][0]].first_id
        while len(sample_edge_ids) > 0:
            # ノードidが含まれるエッジidを探す
            for i in range(len(sample_edge_ids)):
                edge_id = sample_edge_ids[i]
                if self.edges[edge_id].first_id == next_node_id:
                    draw_edge_ids.append([edge_id, '+'])
                    next_node_id = self.edges[edge_id].end_id
                    sample_edge_ids.pop(i)
                    break
                elif self.edges[edge_id].end_id == next_node_id:
                    draw_edge_ids.append([edge_id, '-'])
                    next_node_id = self.edges[edge_id].first_id
                    sample_edge_ids.pop(i)
                    break
        print(draw_edge_ids)
        return draw_edge_ids

    def write(self):
        doc = xml.dom.minidom.Document()

        root = doc.createElement('rcr:map')
        doc.appendChild(root)

        subnode_attr1 = doc.createAttribute('xmlns:rcr')
        subnode_attr1.value = 'urn:roborescue:map:gml'
        root.setAttributeNode(subnode_attr1)
        subnode_attr2 = doc.createAttribute('xmlns:xlink')
        subnode_attr2.value = 'http://www.w3.org/1999/xlink'
        root.setAttributeNode(subnode_attr2)
        subnode_attr3 = doc.createAttribute('xmlns:gml')
        subnode_attr3.value = 'http://www.opengis.net/gml'
        root.setAttributeNode(subnode_attr3)

        nodelist = doc.createElement('rcr:nodelist')
        root.appendChild(nodelist)
        # node書き出し
        for node_id in self.nodes:
            Node = doc.createElement('gml:Node')
            subnode_attr1 = doc.createAttribute('gml:id')
            subnode_attr1.value = str(node_id)
            Node.setAttributeNode(subnode_attr1)
            nodelist.appendChild(Node)

            pointProperty = doc.createElement('gml:pointProperty')
            Node.appendChild(pointProperty)
            Point = doc.createElement('gml:Point')
            pointProperty.appendChild(Point)
            coordinates = doc.createElement('gml:coordinates')
            Point.appendChild(coordinates)
            coordinates.appendChild(
                doc.createTextNode(str(float(self.nodes[node_id].x)) + ',' + str(float(self.nodes[node_id].y))))

        #############################################################################################

        edgelist = doc.createElement('rcr:edgelist')
        root.appendChild(edgelist)
        # edge書き出し
        for edge_id in self.edges:
            Edge = doc.createElement('gml:Edge')
            subnode_attr1 = doc.createAttribute('gml:id')
            subnode_attr1.value = str(edge_id)
            Edge.setAttributeNode(subnode_attr1)

            edgelist.appendChild(Edge)
            directedNode = doc.createElement(
                'gml:directedNode orientation="-" xlink:href="#' + str(self.edges[edge_id].first_id) + '"')

            Edge.appendChild(directedNode)

            directedNode = doc.createElement(
                'gml:directedNode orientation="+" xlink:href="#' + str(self.edges[edge_id].end_id) + '"')
            Edge.appendChild(directedNode)

        #############################################################################################

        buildinglist = doc.createElement('rcr:buildinglist')
        root.appendChild(buildinglist)
        # building書き出し
        print("draw building")
        for building_id in self.buildings:
            building = doc.createElement('rcr:building')
            subnode_attr1 = doc.createAttribute('gml:id')
            subnode_attr1.value = str(building_id)
            building.setAttributeNode(subnode_attr1)
            buildinglist.appendChild(building)

            Face = doc.createElement('gml:Face')
            subnode_attr1 = doc.createAttribute('rcr:floors')
            subnode_attr1.value = str(1)
            Face.setAttributeNode(subnode_attr1)

            subnode_attr2 = doc.createAttribute('rcr:buildingcode')
            subnode_attr2.value = str(0)
            Face.setAttributeNode(subnode_attr2)

            subnode_attr2 = doc.createAttribute('rcr:buildingcode')
            subnode_attr2.value = str(0)
            Face.setAttributeNode(subnode_attr2)

            subnode_attr3 = doc.createAttribute('rcr:importance')
            subnode_attr3.value = str(1)
            Face.setAttributeNode(subnode_attr3)

            buildinglist.appendChild(Face)

            building.appendChild(Face)

            # edgeを描画できるようにする
            edge_ids = self.buildings[building_id].edge_ids
            draw_edge_data = self.get_draw_edges(edge_ids)
            # print(draw_edge_data)

            # buildingの描画用edgeを回す
            for i in range(len(draw_edge_data)):
                edge_id = draw_edge_data[i][0]
                vector = draw_edge_data[i][1]
                if edge_id in self.buildings[building_id].neighbor:
                    directedEdge = doc.createElement(
                        'gml:directedEdge orientation="' + str(vector) + '" xlink:href="#' + str(
                            edge_id) + '" rcr:neighbour="' + str(self.buildings[building_id].neighbor[edge_id]) + '"')
                    Face.appendChild(directedEdge)
                else:
                    directedEdge = doc.createElement(
                        'gml:directedEdge orientation="' + str(vector) + '" xlink:href="#' + str(edge_id) + '"')
                    Face.appendChild(directedEdge)

        #############################################################################################

        roadlist = doc.createElement('rcr:roadlist')
        root.appendChild(roadlist)
        # road書き出し
        print("draw road")
        for road_id in self.roads:
            road = doc.createElement('rcr:road')
            subnode_attr1 = doc.createAttribute('gml:id')
            subnode_attr1.value = str(road_id)
            road.setAttributeNode(subnode_attr1)
            roadlist.appendChild(road)

            Face = doc.createElement('gml:Face')

            roadlist.appendChild(Face)

            road.appendChild(Face)

            # edgeを描画できるようにする
            edge_ids = self.roads[road_id].edge_ids

            # roadの描画用edgeを回す
            draw_edge_data = self.get_draw_edges(edge_ids)
            for i in range(len(draw_edge_data)):
                edge_id = draw_edge_data[i][0]
                vector = draw_edge_data[i][1]
                if edge_id in self.roads[road_id].neighbor:
                    directedEdge = doc.createElement(
                        'gml:directedEdge orientation="' + str(vector) + '" xlink:href="#' + str(
                            edge_id) + '" rcr:neighbour = "' + str(self.roads[road_id].neighbor[edge_id]) + '"')
                    Face.appendChild(directedEdge)
                else:
                    directedEdge = doc.createElement(
                        'gml:directedEdge orientation="' + str(vector) + '" xlink:href="#' + str(edge_id) + '"')
                    Face.appendChild(directedEdge)

        f1 = open(self.path, 'w')
        f1.write(doc.toprettyxml())
        f1.close()
