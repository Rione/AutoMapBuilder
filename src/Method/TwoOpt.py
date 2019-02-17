from src import Astar
from src.World import WorldInfo


class TwoOpt:
    def __init__(self, world_info: WorldInfo):
        self.world_info = world_info
        self.astar = Astar.Astar(self.world_info.g_nodes)

    def calc(self, target_ids: list):
        pass
