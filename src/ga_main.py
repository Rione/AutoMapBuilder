import copy
import os, sys

sys.path.append(os.getcwd().replace('/src', ''))

import time

from src import MapReader, ScenarioReader, GraphDrawer, Astar

# よくばり法
from src.Method import Greedy
from src.Method import GeneticAlgorithm

MAP_NAME = 'sakae'

if __name__ == '__main__':
    map = MapReader.MapReader(MAP_NAME)
    reader = ScenarioReader.ScenarioReader(MAP_NAME)

    scenarios = reader.scenario_reader()
    world_info = map.build_map()
    graph_info = map.build_graph()

    astar = Astar.Astar(map.world_info.g_nodes)
    greedy = Greedy.Greedy(map.world_info)
    drawer = GraphDrawer.GraphDrawer(world_info.g_nodes)

    # 市民の除くエージェント＆避難所をリストアップ
    location_ids = []
    for scenario in scenarios:
        # 避難所
        if scenario[0] == 'refuge':
            location_ids.append(scenario[1])
        # FB
        if scenario[0] == 'firebrigade':
            location_ids.append(scenario[1])
        # PF
        if scenario[0] == 'policeforce':
            location_ids.append(scenario[1])
        # AT
        if scenario[0] == 'ambulanceteam':
            location_ids.append(scenario[1])

    result = greedy.calc(location_ids)

    # 遺伝的アルゴリズム
    genetic = GeneticAlgorithm.GeneticAlgorithm(map.world_info)
    opt_route = genetic.calc(result)
