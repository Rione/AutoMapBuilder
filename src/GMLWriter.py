from src.World import Building, Road


class GMLWrite:
    def __init__(self, path: str):
        self.path = path

    def write(self, building_list: dict, road_list: dict):
        nodes = {}
        for building_id in building_list:
            for edge_id in building_list[building_id].edges:
                f_node = building_list[building_id].edges[edge_id].first
                nodes.setdefault(f_node.id, f_node)
                e_node = building_list[building_id].edges[edge_id].end
                nodes.setdefault(e_node.id, e_node)

        for road_id in road_list:
            for edge_id in building_list[road_id].edges:
                f_node = building_list[road_id].edges[edge_id].first
                nodes.setdefault(f_node.id, f_node)
                e_node = building_list[road_id].edges[edge_id].end
                nodes.setdefault(e_node.id, e_node)
