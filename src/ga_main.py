import copy
import os, sys

sys.path.append(os.getcwd().replace('/src', ''))

import time

from src import MapReader, ScenarioReader, GraphDrawer, Astar

# よくばり法
from src.Method import Greedy, TwoOpt
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

    # result = greedy.calc(location_ids, '')
    r_list = []
    for i in range(10):
        # 遺伝的アルゴリズム
        genetic = GeneticAlgorithm.GeneticAlgorithm(map.world_info)
        genetic_route = genetic.calc(location_ids)

        # 2-opt法
        # two_opt = TwoOpt.TwoOpt(map.world_info)
        # two_opt_route = two_opt.calc(genetic_route, '')

        route = astar.interpolation(genetic_route)
        print(route[0])
        r_list.append(route[0])
        with open('./t_oder_two', mode='a') as f:
            f.writelines(str(route[0]) + '\n')
'''

    drawer.map_register(graph_info.branch_list)
    drawer.route_register(route[1])
    for id in location_ids:
        drawer.node_register(id)
    drawer.show_plt()

'''
