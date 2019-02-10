import Node


class Edge:
    def __init__(self, id: int, first: Node, end: Node):
        self.id = id
        self.first = first
        self.end = end
        self.x = (first.x + end.x) / 2
        self.y = (first.y + end.y) / 2
