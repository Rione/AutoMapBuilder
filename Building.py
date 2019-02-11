class Building:
    def __init__(self, id: int, edges: list, neighbour: list):
        self.id = id
        self.edges = edges
        self.neighbour = neighbour

        # 重心
        sum_x = 0
        sum_y = 0
        for edge in edges:
            sum_x += edge.x
            sum_y += edge.y
        self.x = sum_x / len(edges)
        self.y = sum_y / len(edges)
