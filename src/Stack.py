class Stack:
    def __init__(self):
        self.stack = []

    def push(self, e):
        self.stack.append(e)
        return self.stack

    def pop(self):
        return self.stack.pop()

    def empty(self):
        if len(self.stack) <= 0:
            return True
        else:
            return False
