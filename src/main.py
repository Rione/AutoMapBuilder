from src import MapReader

if __name__ == '__main__':
    map = MapReader.MapReader('sakae')
    map.build_graph()
