import math

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

        buildinglist = doc.createElement('rcr:buildinglist')
        root.appendChild(buildinglist)
        f1 = open(self.path, 'w')
        f1.write(doc.toprettyxml())
        f1.close()
        w
        # building書き出し
        for building_id in self.buildings:
            building = doc.createElement('gml:building')
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
            # buildingのedgeを回す
            for edge_id in self.buildings[building_id].edges:
                if edge_id in self.buildings[building_id].neighbor_id:
                    directedEdge = doc.createElement(
                        'gml:directedEdge orientation="+" xlink:href="#' + str(
                            edge_id) + '" rcr:neighbour="' + str(
                            self.buildings[building_id].neighbor_id[edge_id]) + '"')
                    Face.appendChild(directedEdge)
                else:
                    directedEdge = doc.createElement(
                        'gml:directedEdge orientation="+" xlink:href="#' + str(edge_id) + '"')
                    Face.appendChild(directedEdge)

        roadlist = doc.createElement('rcr:roadlist')
        root.appendChild(roadlist)
        # road書き出し
        for road_id in self.roads:
            road = doc.createElement('gml:road')
            subnode_attr1 = doc.createAttribute('gml:id')
            subnode_attr1.value = str(road_id)
            road.setAttributeNode(subnode_attr1)
            buildinglist.appendChild(road)

            Face = doc.createElement('gml:Face')
            subnode_attr1 = doc.createAttribute('rcr:floors')
            subnode_attr1.value = str(1)
            Face.setAttributeNode(subnode_attr1)

            subnode_attr2 = doc.createAttribute('rcr:buildingcode')
            subnode_attr2.value = str(0)
            Face.setAttributeNode(subnode_attr2)

            subnode_attr2 = doc.createAttribute('rcr:importance')
            subnode_attr2.value = str(1)
            Face.setAttributeNode(subnode_attr2)

            subnode_attr3 = doc.createAttribute('rcr:importance')
            subnode_attr3.value = str(1)
            Face.setAttributeNode(subnode_attr3)

            roadlist.appendChild(Face)

            road.appendChild(Face)

            # roadのedgeを回す
            for edge_id in self.roads[road_id].edges:
                if edge_id in self.roads[road_id].neighbor_ids:
                    directedEdge = doc.createElement(
                        'gml:directedEdge orientation="+" xlink:href="#' + str(
                            edge_id) + '" rcr:neighbour = "' + str(
                            self.roads[road_id].neighbor_ids[edge_id]) + '"')
                    Face.appendChild(directedEdge)
                else:
                    directedEdge = doc.createElement(
                        'gml:directedEdge orientation="+" xlink:href="#' + str(edge_id) + '"')
                    Face.appendChild(directedEdge)

        f1 = open(self.path, 'w')
        f1.write(doc.toprettyxml())
        f1.close()
