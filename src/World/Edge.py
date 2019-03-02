from src.World import Node


class Edge:
    def __init__(self, id: int, first_id: int, end_id: int):
        # endの方がx座標が大きい
        self.id = id
        self.first_id = first_id
        self.end_id = end_id
