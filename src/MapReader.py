import xml.etree.ElementTree as ET

from src.Graph import G_Node
from src.World import Edge, Road, Node, Building, WorldInfo


class MapReader:
    def __init__(self, map_name: str):
        tree = ET.ElementTree(file='./map/' + map_name + '/map/map.gml')
        root = tree.getroot()

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

        print(self.world_info.building_data.get(9601))

    def build_graph(self):
        nodes = []
        for id in self.world_info.building_data:
            building = self.world_info.building_data.get(id)
            nodes.append(building)
        for id in self.world_info.road_data:
            road = self.world_info.road_data.get(id)
            nodes.append(road)
        print(nodes)


kobe = MapReader('kobe')
kobe.build_graph()
