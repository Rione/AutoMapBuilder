import os
import sys

import numpy as np

sys.path.append(os.getcwd().replace('/src', ''))

from World import Building

MAP_WIDTH = 10
MAP_HEIGHT = 10


# 空白:-1
# 道:0
# 建物:1~

# 最初に配列にランダムに拠点を作る
# その時に別にIDとノード情報を保持するテーブルを作成
# 次に陣地をうばうように建物を広げていく
# その際にノード情報を更新していく(無い座標は追加、ある座標は削除)

class CreateMap:
    def __init__(self):
        self.map_array = np.full((MAP_WIDTH, MAP_HEIGHT), -1)

    def get_key(self, x: int, y: int):
        if x > y:
            return 10 ** len(str(x)) * y + x
        else:
            return 10 ** len(str(y)) * x + y

    def judge_neighbor(self, map_array: np.ndarray, distance: int, target_x: int, target_y: int):
        # 値が入っているかどうか確認
        # 指定場所に値が入っているか
        if not map_array[target_x][target_y] == -1:
            return False

        # xがはみ出していないか
        if target_x - distance >= 0:
            # 上下左右斜めで確認
            if not map_array[target_x - distance][target_y] == -1:
                return False
            if target_y - distance < 0 and not map_array[target_x - distance][target_y - distance] == -1:
                return False
            if target_y + distance > len(map_array) and not map_array[target_x - distance][target_y + distance] == -1:
                return False

        if target_y - distance < 0 and not map_array[target_x][target_y - distance] == -1:
            return False
        if target_y + distance > len(map_array) and not map_array[target_x][target_y + distance] == -1:
            return False

        if target_x + distance < len(map_array):
            if not map_array[target_x + distance][target_y] == -1:
                return False
            if target_y - distance < 0 and not map_array[target_x + distance][target_y - distance] == -1:
                return False
            if target_y + distance > len(map_array) and not map_array[target_x + distance][target_y + distance] == -1:
                return False

        return True

    def create_array_map(self):
        building_id = 1
        building_count = 0
        # 仮に１０個
        while building_count <= 3:
            # 配列にランダムに拠点を設定(ただし隣接は禁止)
            # x,yをランダムで選択
            target_x = np.random.randint(10)
            target_y = np.random.randint(10)

            # 隣接がないか確認
            if self.judge_neighbor(self.map_array, 1, target_x, target_y):
                self.map_array[target_x][target_y] = building_id
                building_id += 1
                building_count += 1


create = CreateMap()
create.create_array_map()
print(create.map_array)
