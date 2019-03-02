from src.World import Node


class Building:
    def __init__(self, id: int):
        self.id = id
        self.neighbor_id = {}
        self.edges = {}

    def update_nodes(self, edge_keys: dict):
        for key in edge_keys:
            if key in self.edges:
                # 同じ座標があった場合
                del self.edges[key]
            else:
                # まだ同じ座標が含まれていない場合
                self.edges.setdefault(key, edge_keys[key])
