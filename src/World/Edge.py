from src.World import Node


class Edge:
    def __init__(self, id: int, first: Node, end: Node):
        self.id = id
        self.first = first
        self.end = end
