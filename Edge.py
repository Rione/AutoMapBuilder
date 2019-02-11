import Node
import numpy as np


class Edge:
    def __init__(self, id: int, first_id: int, end_id: int):
        self.id = id
        self.first_id = first_id
        self.end_id = end_id
        self.x = 0
        self.y = 0
        self.nodes = np.asarray([])

    # nodeデータと関連付け
    def registry_data(self, first: Node, end: Node):
        self.nodes = np.append(self.nodes, first)
        self.nodes = np.append(self.nodes, first)
        self.x = (first.x + end.x) / 2
        self.y = (first.y + end.y) / 2
