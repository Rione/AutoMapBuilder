class Road:
    def __init__(self, id: int, edges: list):
        self.id = id
        self.neighbor_ids = {}
        self.edges = edges