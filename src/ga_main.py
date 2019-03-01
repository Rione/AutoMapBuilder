import os, sys, copy

sys.path.append(os.getcwd().replace('/src', ''))

from src import MapReader, ScenarioReader, GraphDrawer, Astar

# よくばり法
from src.Method import Greedy, TwoOpt
from src.Method import GeneticAlgorithm

MAP_NAME = 'sakae'

map = MapReader.MapReader(MAP_NAME)
reader = ScenarioReader.ScenarioReader(MAP_NAME)

world_info = map.build_map()
graph_info = map.build_graph()
drawer = GraphDrawer.GraphDrawer(world_info.g_nodes)

location_ids = []


def main():
    astar = Astar.Astar(map.world_info.g_nodes)
    scenarios = reader.scenario_reader()

    # 市民の除くエージェント＆避難所をリストアップ
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

    cost_table = astar.create_cost_table(MAP_NAME, location_ids)

    # result = greedy.calc(location_ids, '')
    # 遺伝的アルゴリズム
    genetic = GeneticAlgorithm.GeneticAlgorithm(map.world_info, cost_table)
    genetic_route = genetic.calc(location_ids)

    # 2-opt法
    # two_opt = TwoOpt.TwoOpt(map.world_info)
    # two_opt_route = two_opt.calc(genetic_route, '')

    route = astar.interpolation(genetic_route)
    print(route[0])
    return route


if __name__ == '__main__':
    route = main()
    drawer.map_register(graph_info.branch_list)
    drawer.route_register(route[1])
    for id in location_ids:
        drawer.node_register(id)
    drawer.show_plt()
