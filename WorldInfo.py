import Node, Edge, Road, Building


class WorldInfo:
    def __init__(self, nodes: list, edges: list, buildings: list, roads: list):
        self.node_data = {}
        for node in nodes:
            self.node_data.setdefault(node.id, node)

        self.edge_data = {}
        for edge in edges:
            edge.registry_node(self.node_data.get(edge.first_id), self.node_data.get(edge.end_id))
            self.edge_data.setdefault(edge.id, edge)

        self.building_data = {}
        for building in buildings:
            b_edges = []
            for edge_id in building.edge_ids:
                b_edges.append(self.edge_data.get(edge_id))
            building.registry_edge(b_edges)
            self.building_data.setdefault(building.id, building)

        self.road_data = {}
        for road in roads:
            r_edges = []
            for edge_id in road.edge_ids:
                r_edges.append(self.edge_data.get(edge_id))
            road.registry_edge(r_edges)
            self.road_data.setdefault(road.id, road)

        # neighbour登録
        # road
        for building_id in self.building_data:
            neighbour_list = []
            for neighbour_id in self.building_data.get(building_id).neighbour_ids:
                neighbour_list.append(self.road_data.get(neighbour_id))
            self.building_data.get(building_id).registry_neighbour(neighbour_list)

        # building
        for road_id in self.road_data:
            neighbour_list = []
            for neighbour_id in self.road_data.get(road_id).neighbour_ids:
                neighbour_list.append(self.building_data.get(neighbour_id))
            self.road_data.get(road_id).registry_neighbour(neighbour_list)
