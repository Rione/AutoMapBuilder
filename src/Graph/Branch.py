from src.Graph import G_Node


class Branch:
    def __init__(self, node: G_Node, distance: float):
        self.node = node
        self.distance = distance
