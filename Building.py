import numpy as np


class Building:
    def __init__(self, id: int, edge_ids: list, neighbour_ids: list):
        self.id = id
        self.edge_ids = edge_ids
        self.neighbour_ids = neighbour_ids
        self.x = 0
        self.y = 0
        self.edges = np.asarray([])

    def registry_data(self, edges: list):
        # 重心
        sum_x = 0
        sum_y = 0
        for edge in edges:
            sum_x += edge.x
            sum_y += edge.y
            self.edges = np.append(self.edges, edge)
        self.x = sum_x / len(edges)
        self.y = sum_y / len(edges)
