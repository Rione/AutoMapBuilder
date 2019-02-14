from src.Graph import Branch
import numpy as np


class G_Node:
    def __init__(self, element):
        self.element = element
        self.branch = np.asarray([])
