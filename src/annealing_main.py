import os, sys

sys.path.append(os.getcwd().replace('/src', ''))

from src import Astar, GraphDrawer, ScenarioReader, MapReader
from src.Method import Greedy, TwoOpt, Annealing

MAP_NAME = 'sakae'

map = MapReader.MapReader(MAP_NAME)
world_info = map.build_map()
graph_info = map.build_graph()
drawer = GraphDrawer.GraphDrawer(world_info.g_nodes)

location_ids = []


def main():
    reader = ScenarioReader.ScenarioReader(MAP_NAME)

    scenarios = reader.scenario_reader()

    astar = Astar.Astar(map.world_info.g_nodes)

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

    greedy = Greedy.Greedy(map.world_info, cost_table)

    greedy_route = greedy.calc(location_ids)

    annealing = Annealing.Annealing(map.world_info, cost_table)
    result = annealing.calc(greedy_route)
    route = astar.interpolation(result)
    print(route[0])
    return route


if __name__ == '__main__':
    route = main()

    drawer.map_register(graph_info.branch_list)
    drawer.route_register(route[1])
    for id in location_ids:
        drawer.node_register(id)
    drawer.show_plt()
