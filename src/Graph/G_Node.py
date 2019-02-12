from src.Graph import Branch
import numpy as np


class G_Node:
    def __init__(self, id: int, x: float, y: float):
        self.id = id
        self.x = x
        self.y = y
        self.branch = np.asarray([])
