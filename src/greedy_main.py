import copy
import sys
import time

from src import MapReader, ScenarioReader, GraphDrawer, Astar

# よくばり法
from src.Method import Greedy

if __name__ == '__main__':
    map = MapReader.MapReader('sakae')
    reader = ScenarioReader.ScenarioReader('sakae')

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
    print(result[0])
    route = result[1]

    route = drawer.interpolation(route, astar)

    drawer.map_register(graph_info.branch_list)
    drawer.route_register(route)
    drawer.show_plt()
