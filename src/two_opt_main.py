import os

from src import Astar, GraphDrawer, ScenarioReader, MapReader
from src.Method import Greedy, TwoOpt

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
    route = []
    with open(os.getcwd().replace('/src', '') + '/map/' + MAP_NAME + '/map/route') as f:
        for s_line in f:
            route = eval(s_line)
    '''
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
    '''

    two_opt = TwoOpt.TwoOpt(map.world_info)
    result = two_opt.calc(route)
    route = drawer.interpolation(result, astar)
    print(route[0])
    print(route[1])
    drawer.map_register(graph_info.branch_list)
    drawer.route_register(route[1])
    drawer.show_plt()
