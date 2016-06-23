function trim_at_first_blank(s) {
  if(s.indexOf(' ') > -1) {
    return s.substring(0, s.indexOf(' ')) + ' ...';
  } else {
    return s;
  }
}
