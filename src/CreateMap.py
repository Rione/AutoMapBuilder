import os
import sys

import numpy as np

sys.path.append(os.getcwd().replace('/src', ''))

from World import Building

MAP_WIDTH = 100
MAP_HEIGHT = 100


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

    def neighbor_judge(self, map_array: np.ndarray, distance: int, target_x: int, target_y: int):
        # 値が入っているかどうか確認
        # 指定場所に値が入っているか
        if not map_array[target_x][target_y] == -1:
            return False

        # 上下左右斜めで確認
        if not map_array[target_x - 1][target_y] == -1:
            return False
        if not map_array[target_x + 1][target_y] == -1:
            return False
        if not map_array[target_x][target_y - 1] == -1:
            return False
        if not map_array[target_x][target_y + 1] == -1:
            return False
        if not map_array[target_x - 1][target_y - 1] == -1:
            return False
        if not map_array[target_x + 1][target_y - 1] == -1:
            return False
        if not map_array[target_x - 1][target_y + 1] == -1:
            return False
        if not map_array[target_x + 1][target_y + 1] == -1:
            return False

        return True


create = CreateMap()
print(type(create.map_array))
# 配列にランダムに拠点を設定(ただし隣接は禁止)
