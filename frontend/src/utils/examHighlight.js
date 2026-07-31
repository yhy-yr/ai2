const IMPORTANT_KEYWORDS = ['明显增高', '异常', '阳性', '感染', '升高', '偏高', '炎症']
const ATTENTION_KEYWORDS = ['偏低', '降低', '减少']
const NORMAL_KEYWORDS = ['未见明显异常', '正常', '阴性']
const ALL_KEYWORDS = Array.from(new Set([...IMPORTANT_KEYWORDS, ...ATTENTION_KEYWORDS, ...NORMAL_KEYWORDS]))
  .sort((a, b) => b.length - a.length)

export function highlightExamResult(text) {
  const value = text ? String(text) : ''
  if (!value) {
    return [{ text: '无', type: 'plain' }]
  }

  const segments = []
  let index = 0

  while (index < value.length) {
    const keyword = ALL_KEYWORDS.find((item) => value.startsWith(item, index))
    if (!keyword) {
      const nextIndex = nextKeywordIndex(value, index + 1)
      segments.push({
        text: value.slice(index, nextIndex),
        type: 'plain'
      })
      index = nextIndex
      continue
    }

    segments.push({
      text: keyword,
      type: keywordType(keyword)
    })
    index += keyword.length
  }

  return segments
}

export function analyzeExamResult(...texts) {
  const value = texts.filter(Boolean).join('，')
  const segments = highlightExamResult(value)
  const hasImportant = segments.some((segment) => segment.type === 'important')
  const hasAttention = segments.some((segment) => segment.type === 'attention')
  const hasNormal = segments.some((segment) => segment.type === 'normal')

  if (hasImportant || hasAttention) {
    return {
      type: 'warning',
      message: '检查结果存在需关注项目，请结合医生意见判断。'
    }
  }

  if (hasNormal) {
    return {
      type: 'normal',
      message: '检查结果整体较稳定，请以医生解释为准。'
    }
  }

  return null
}

function nextKeywordIndex(text, start) {
  const indexes = ALL_KEYWORDS
    .map((keyword) => text.indexOf(keyword, start))
    .filter((item) => item >= 0)
  return indexes.length ? Math.min(...indexes) : text.length
}

function keywordType(keyword) {
  if (IMPORTANT_KEYWORDS.includes(keyword) && !NORMAL_KEYWORDS.includes(keyword)) {
    return 'important'
  }
  if (ATTENTION_KEYWORDS.includes(keyword)) {
    return 'attention'
  }
  return 'normal'
}
