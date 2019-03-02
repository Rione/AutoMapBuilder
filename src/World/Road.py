class Road:
    def __init__(self, id: int, edge_ids: list):
        self.id = id
        self.neighbor = {}
        self.edge_ids = edge_ids