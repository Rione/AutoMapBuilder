class Building:
    def __init__(self, id: int, x_index: int, y_index: int):
        self.id = id
        self.x_index = x_index
        self.y_index = y_index
        self.nodes = set()
        self.positions = []
