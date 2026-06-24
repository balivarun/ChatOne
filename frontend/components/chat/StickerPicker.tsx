'use client';
import { useState } from 'react';

const STICKER_CATEGORIES = {
  '😀 Happy': ['😀','😃','😄','😁','😆','😊','😍','🥰','😎','🤩','😜','🥳','🎉','🔥','💯','👏','🙌','✌️','💪','👍'],
  '❤️ Love': ['❤️','🧡','💛','💚','💙','💜','🖤','💕','💞','💓','💗','💖','💘','💝','😘','😗','😚','😙','🫶','💌'],
  '😢 Sad': ['😢','😭','😔','😟','😞','😣','😩','😫','🥺','😥','😓','😰','😨','😧','😦','😮','😯','😲','🙁','☹️'],
  '😡 Reactions': ['😡','🤬','😤','😠','🤯','🤔','🤨','😑','😶','🙄','😒','😏','😬','🤐','🤫','🤭','😈','👿','💀','🤡'],
  '🐶 Animals': ['🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐨','🐯','🦁','🐮','🐷','🐸','🐵','🦄','🐔','🐧','🐦','🦋'],
  '🎮 Fun': ['🎮','🎯','🎲','🎸','🎵','🎶','🎤','🎬','📸','⚽','🏀','🎾','⚾','🏈','🏆','🥇','🎊','🎁','🚀','🌈'],
};

interface Props {
  onSelect: (sticker: string) => void;
}

export default function StickerPicker({ onSelect }: Props) {
  const [activeCategory, setActiveCategory] = useState(Object.keys(STICKER_CATEGORIES)[0]);

  return (
    <div className="w-72 bg-white dark:bg-gray-900 rounded-xl shadow-xl border border-black/10 dark:border-white/10 overflow-hidden">
      {/* Category tabs */}
      <div className="flex overflow-x-auto scrollbar-hide border-b border-black/10 dark:border-white/10">
        {Object.keys(STICKER_CATEGORIES).map((cat) => (
          <button
            key={cat}
            onClick={() => setActiveCategory(cat)}
            className={`flex-shrink-0 px-3 py-2 text-lg transition-colors ${
              activeCategory === cat
                ? 'bg-accent/10 border-b-2 border-accent'
                : 'hover:bg-black/5 dark:hover:bg-white/5'
            }`}
            title={cat}
          >
            {cat.split(' ')[0]}
          </button>
        ))}
      </div>

      {/* Sticker grid */}
      <div className="grid grid-cols-5 gap-1 p-3 max-h-52 overflow-y-auto">
        {STICKER_CATEGORIES[activeCategory as keyof typeof STICKER_CATEGORIES].map((sticker, i) => (
          <button
            key={i}
            onClick={() => onSelect(sticker)}
            className="text-3xl hover:bg-black/5 dark:hover:bg-white/5 rounded-lg p-1 transition-transform hover:scale-125 flex items-center justify-center"
          >
            {sticker}
          </button>
        ))}
      </div>
    </div>
  );
}
