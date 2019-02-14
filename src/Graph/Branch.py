from src.Graph import G_Node


class Branch:
    def __init__(self, to_node: int, cost: float):
        self.to_node = to_node
        self.cost = cost
