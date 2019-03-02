from src.World import Node


class Building:
    def __init__(self, id: int, edge_ids: list):
        self.id = id
        self.neighbor_id = {}
        self.edge_ids = edge_ids

    def update_nodes(self, edge_ids: list):
        for edge_id in edge_ids:
            if edge_id in self.edge_ids:
                # 同じ座標があった場合
                self.edge_ids.remove(edge_id)
            else:
                # まだ同じ座標が含まれていない場合
                self.edge_ids.append(edge_id)
